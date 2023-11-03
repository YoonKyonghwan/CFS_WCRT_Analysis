import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.cm import coolwarm
from mpl_toolkits.mplot3d import Axes3D
import re
from sklearn.metrics import confusion_matrix
import os
import glob


def read_log_from_file(file_path):
    with open(file_path, 'r') as f:
        log_data = f.read()
    return log_data

def get_subLog(task_id, response_time_us, log_data):
    # Build the regex pattern for the completion time of the given task
    completion_pattern = fr"Task {task_id} completed at time (\d+) with RT {response_time_us}(\d+)"
    # Find the completion time of the task
    completion_match = re.search(completion_pattern, log_data)
    if completion_match:
        completion_time = int(completion_match.group(1))
        # Find the whole line of the completion event
        completion_start = log_data.rfind('\n', 0, completion_match.start()) + 1
        completion_end = completion_match.end()
    else:
        return "Completion time not found for the given task and response time."

    # Search for the last release before the completion
    # Find all releases before the completion time and get the last one
    releases = [m for m in re.finditer(fr"Tasks {task_id} Released at time (\d+)", log_data[:completion_start])]
    if releases:
        # Get the last release match
        last_release_match = releases[-1]
        # Find the whole line of the last release event
        release_start = log_data.rfind('\n', 0, last_release_match.start()) + 1
    else:
        # If there's no release found, we consider the start of the log
        releases_v2 = [m for m in re.finditer("------------------------------", log_data[:completion_start])]
        if releases_v2:
            last_release_match = releases_v2[-1]
            release_start = log_data.rfind('\n', 0, last_release_match.start()) + 1

    # Extract the sublog between the lines of the last release and completion
    sublog = log_data[release_start:completion_end]

    return sublog.strip()

# Parse the log data
def parse_log(log_data):
    task_starts = re.findall(r"Task (\d+)\(vruntime:.+\) started to run at time (\d+)", log_data)
    task_durations = re.findall(r"Task \d+ spends (\d+) ns from \d+ to \d+\[vruntime_increment:.+\]", log_data)
    task_releases = re.findall(r"Tasks (\d+) Released at time (\d+)", log_data)
    task_completions = re.findall(r"Task (\d+) completed at time (\d+) with RT \d+", log_data)

    tasks = [(int(task), int(start), int(duration)) for (task, start), duration in zip(task_starts, task_durations)]
    releases = [(int(task), int(time)) for task, time in task_releases]
    completions = [(int(task), int(time)) for task, time in task_completions]
    
    return tasks, releases, completions

# Convert nanoseconds to seconds
def ns_to_us(ns):
    return ns / 1000

# Plotting function
def plot_gantt(task_data, releases, completions):
    fig, ax = plt.subplots(figsize=(12, 5))
    
    for task, start, duration in task_data:
        ax.broken_barh([(ns_to_us(start), ns_to_us(duration))], (task - 0.4, 0.8), facecolors='tab:blue')

    # Mark task releases
    for task, release_time in releases:
        ax.plot(ns_to_us(release_time), task, 'go')

    # Mark task completions
    for task, completion_time in completions:
        ax.plot(ns_to_us(completion_time), task, 'ro')
        
    # add a legend for the release and completion markers
    ax.plot([], [], 'go', label='Task Release')
    ax.plot([], [], 'ro', label='Task Completion')
    
    # count the number of tasks
    num_tasks = len(set([task for task, _, _ in task_data]))
    print(num_tasks)

    ax.set_xlabel('Time (us)')
    ax.set_ylabel('Task ID')
    ax.set_yticks(range(1, num_tasks + 1))
    ax.set_yticklabels([f'Task {task}' for task in range(1, num_tasks + 1)])
    ax.set_title('Task Execution Timeline')
    ax.legend()
    plt.show()


# Create a 3D plot to show the relationship between 'numTasks', 'utilization', and 'timeConsumptionGap'
def show_3D_plot(time_data, z, title):
    fig = plt.figure(figsize=(10, 6))
    ax = fig.add_subplot(111, projection='3d')

    x = time_data['numTasks']
    y = time_data['utilization']
    
    scatter = ax.scatter(x, y, z, c=z, cmap=coolwarm, marker='o')
    ax.set_xlabel('Number of Tasks')
    ax.set_ylabel('Utilization')
    ax.set_zlabel('Time Consumption (us)')
    ax.set_title(title)

    # Add a colorbar
    cbar = fig.colorbar(scatter, ax=ax, pad=0.1)
    cbar.set_label('Time Consumption (us)')

    plt.show()
    
    # a heatmap to show a gap between simulator and proposed
def show_heatmap(time_data, values, title):
    pivot = time_data.pivot(index='numTasks', columns='utilization', values=values)
    plt.figure(figsize=(6, 3))
    plt.imshow(pivot, cmap='coolwarm', aspect='auto', interpolation='nearest')
    plt.title(title)
    plt.colorbar(label="Time Consumption (us)")
    plt.xlabel('Utilization')
    plt.ylabel('Number of Tasks')
    plt.xticks(range(len(pivot.columns)), pivot.columns, rotation=45)
    plt.yticks(range(len(pivot.index)), pivot.index)
    plt.tight_layout()
    plt.show()
    
def parse_time_data(summary_result_path):
    df = pd.read_csv(summary_result_path, sep=",")
    return df.groupby(['numTasks', 'utilization'])[['simulator_timeConsumption(us)', 'proposed_timeConsumption(us)']].mean().reset_index()

def save_time_data_info_file(time_data, save_file_path):
    time_consumption_summary = time_data[['numTasks', 'utilization', 'proposed_timeConsumption(us)', 'simulator_timeConsumption(us)']]
    time_consumption_summary.to_csv(save_file_path, index=False)
    
def show_bar_chart(time_data, values, title):
    # Create a bar chart to show the relationship between 'numTasks' and 'utilization' and 'proposed_timeConsumption'
    plt.figure(figsize=(12, 6))
    x = range(len(time_data))
    y = time_data[values]

    # Create labels for the x-axis by combining 'numTasks' and 'utilization'
    x_labels = [f'{num_tasks}, {utilization}' for num_tasks, utilization in time_data[['numTasks', 'utilization']].values]

    plt.bar(x, y)
    plt.xlabel('Number of Tasks, Utilization')
    plt.ylabel('Proposed Time Consumption (us)')
    plt.title(title)
    plt.xticks(x, x_labels, rotation=45)
    plt.tight_layout()
    plt.show()
    
def show_box_plot(file_path, values, title):
    data = pd.read_csv(file_path)

    # Group the data by the number of tasks and utilization
    time_data = data.groupby(['numTasks', 'utilization'])
    # Prepare the data for the box plot
    boxplot_data = []
    labels = []

    for (numTasks, utilization), group in time_data:
            time_consumption = group[values]
            boxplot_data.append(time_consumption)
            labels.append(f"nT: {numTasks}, U: {utilization}")

    # Create the box plot
    plt.figure(figsize=(10, 6))
    plt.boxplot(boxplot_data, labels=labels, showfliers=False)
    plt.xticks(rotation=45)
    plt.xlabel('Number of Tasks and Utilization')
    plt.ylabel('Time Consumption (us)')
    plt.title(title)
    plt.tight_layout()

    # Display the plot
    plt.show()
    
    
def check_correntness(input_path, output_path):
    df = pd.read_csv(input_path, sep=",")

    results = []
    combinations = df[['numTasks', 'utilization']].drop_duplicates()

    # Loop through each combination
    for i, (num_tasks, utilization) in combinations.iterrows():
        subset = df[(df['numTasks'] == num_tasks) & (df['utilization'] == utilization)]
        # Calculate confusion matrix for the subset
        confusion = confusion_matrix(subset['simulator_schedulability'], subset['proposed_schedulability'])
        # Calculate True Positives (TP), True Negatives (TN), False Positives (FP), and False Negatives (FN)
        if (confusion.shape == (2, 2)):
            TP = confusion[1, 1]
            TN = confusion[0, 0]
            FP = confusion[0, 1]
            FN = confusion[1, 0]
        else:
            assert confusion.shape == (1, 1)
            if subset['simulator_schedulability'].iloc[0] == False:
                TP = 0
                TN = confusion[0, 0]
                FP = 0
                FN = 0
            else:
                TP = confusion[0, 0]
                TN = 0
                FP = 0
                FN = 0
                
        accuracy = (TP + TN) / (TP + TN + FP + FN)
        # precision = TP / (TP + FP) if (TP + FP) != 0 else 0
        # recall = TP / (TP + FN) if (TP + FN) != 0 else 0

        # Append the results to the list
        results.append([num_tasks, utilization, TP, TN, FP, FN, accuracy])

    # Create a DataFrame to store the results
    results_df = pd.DataFrame(results, columns=['numTasks', 'utilization', 'TP', 'TN', 'FP', 'FN', 'accuracy'])
    results_df = results_df.sort_values(['numTasks', 'utilization'])
    results_df.to_csv(output_path, index=False)
    print(results_df)
    
def show_detail_result(input_path, num_tasks, utilization):
    df = pd.read_csv(input_path, sep=",")
    subset = df[(df['numTasks'] == num_tasks) & (df['utilization'] == utilization)]
    # merge subsets of simulator_schedulability and proposed_schedulability
    print(subset[['simulator_schedulability', 'proposed_schedulability']])


def combine_detail_result(detail_result_dir, numCores, numTasks, utilizations, output_path):
    current_dir = os.getcwd()
    
    results = []
    for num_cores in numCores:
        for num_tasks in numTasks:
            for utilization in utilizations:
    # detail_input_dir = exp_results/
    # file_name sample = exp_results/detail_result/1cores/3tasks/0.2utilization/1cores_3tasks_0.2utilization_0_result.csv
                tmp_detail_result_dir = os.path.join(detail_result_dir, str(num_cores) + 'cores', str(num_tasks) + 'tasks', str(utilization) + 'utilization')
                os.chdir(tmp_detail_result_dir)
                extension = 'csv'
                all_filenames = [i for i in glob.glob('*.{}'.format(extension))]

                # combine all files in the list 
                # append filename as a first column
                for filename in all_filenames:
                    pattern = r'(\d+)cores_(\d+)tasks_(\d+\.\d+)utilization_(\d+)_result.csv'
                    match = re.match(pattern, filename)

                    if match:                        
                        df = pd.read_csv(filename, sep=",", header=None, index_col=0)
                        df = df.iloc[1:] # remove header
                        df['numCores'] = num_cores
                        df['numTasks'] = num_tasks
                        df['utilization'] = utilization
                        df['dataset_index'] = int(match.group(4))
                        
                        results.append(df)
                os.chdir(current_dir)
    
    # add column names
    combined_df = pd.concat(results, axis=0, ignore_index=True)
    combined_df.columns = ['name', 'WCRT(sim)', 'simulator_schedulability', 'WCRT(prop)', 'proposed_schedulability', 'numCores', 'numTasks', 'utilization', 'dataset_index']
    # sort the results by numTasks and utilization
    combined_df = combined_df.sort_values(['numTasks', 'utilization'])
    
    combined_df.to_csv(output_path, index=False)
    
def print_wrong_result(combine_detail_result_path):
    df = pd.read_csv(combine_detail_result_path, sep=",")

    wrong_result = df[(df['WCRT(sim)'] > df['WCRT(prop)']) & (df['WCRT(prop)'] != 0)]
    # wrong_result = df[(df['simulator_schedulability'] == False) & (df['proposed_schedulability'] == True)]

    print(wrong_result[['numTasks', 'utilization', 'dataset_index', 'name', 'WCRT(sim)', 'WCRT(prop)']])