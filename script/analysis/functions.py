import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.cm import coolwarm
from mpl_toolkits.mplot3d import Axes3D
import re
from sklearn.metrics import confusion_matrix
import os


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
    task_executions = re.findall(r"Task (\d+) spends (\d+) ns from (\d+) to \d+\[vruntime_increment:.+\]", log_data)
    task_releases = re.findall(r"Tasks (\d+) Released at time (\d+)", log_data)
    task_completions = re.findall(r"Task (\d+) completed at time (\d+) with RT \d+", log_data)

    # Convert the strings to integers and reorder the tuples
    tasks = [(int(task), int(start), int(duration)) for task, duration, start in task_executions]
    releases = [(int(task), int(time)) for task, time in task_releases]
    completions = [(int(task), int(time)) for task, time in task_completions]
    
    return tasks, releases, completions


# Convert nanoseconds to seconds
def ns_to_us(ns):
    return ns / 1000


# Plotting function
def plot_gantt(task_data, releases, completions):
    fig, ax = plt.subplots(figsize=(15, 5))
    
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
    return

    
def parse_time_data(summary_result_path):
    df = pd.read_csv(summary_result_path, sep=",")
    return df.groupby(['numTasks', 'utilization'])[['simulator_timeConsumption(us)', 'proposed_timeConsumption(us)']].mean().reset_index()


def save_time_data_info_file(time_data, save_file_path):
    time_consumption_summary = time_data[['numTasks', 'utilization', 'proposed_timeConsumption(us)', 'simulator_timeConsumption(us)']]
    time_consumption_summary.to_csv(save_file_path, index=False)
    return


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
    return


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
    return
    
    
    
def show_bar_chart(time_data, values, title, fontsize, xtick_rotation):
    plt.figure(figsize=(12, 6))
    x = range(len(time_data))
    y = time_data[values]

    # Create labels for the x-axis by combining 'numTasks' and 'utilization'
    x_labels = [f'{num_tasks}, {utilization}' for num_tasks, utilization in time_data[['numTasks', 'utilization']].values]

    plt.bar(x, y)
    plt.xlabel('Number of Tasks, Utilization', fontsize=fontsize)
    plt.ylabel('Proposed Time Consumption (us)' ,fontsize=fontsize)
    plt.title(title, fontsize=fontsize)
    plt.xticks(x, x_labels, rotation=xtick_rotation, fontsize=fontsize)
    plt.tight_layout()
    plt.show()
    return
    
    
def show_box_plot_time_analysis(file_path, fontsize, xtick_rotation):
    df = pd.read_csv(file_path, sep=",")
    # df = df[df['proposed_schedulability'] == True]
    time_data = df.groupby(['numTasks', 'utilization'])[['proposed_timeConsumption(us)']]
    # time_data = df.groupby(['numTasks', 'utilization'])[['simulator_timeConsumption(us)', 'proposed_timeConsumption(us)']]
    
    # Prepare the data for the box plot
    boxplot_data = []
    labels = []

    for (numTasks, utilization), group in time_data:
        time_consumption = group['proposed_timeConsumption(us)']
        boxplot_data.append(time_consumption)
        labels.append(f"nT: {numTasks}, U: {utilization}")

    # Create the box plot
    plt.figure(figsize=(12, 8))
    # plt.boxplot(boxplot_data, labels=labels)
    plt.boxplot(boxplot_data, labels=labels, showfliers=False)
    plt.xticks(rotation=xtick_rotation)
    plt.xlabel('Number of Tasks (nT) and Utilization (U)', fontsize=fontsize)
    plt.ylabel('Time Consumption (us)', fontsize=fontsize)
    # plt.title(title, fontsize=fontsize)
    plt.tight_layout()

    plt.xticks(fontsize=fontsize)
    plt.yticks(fontsize=fontsize)

    # save it to pdf 
    plt.savefig("time_boxplot.pdf", bbox_inches='tight', pad_inches=0.3)
    
    # Display the plot
    plt.show()
    
    print("Max : ", df['proposed_timeConsumption(us)'].max(), "Min : ", df['proposed_timeConsumption(us)'].min())
    return
    
    
def check_correctness_simulator(input_path):
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
            assert confusion.shape == (1, 1), "need to check the confusion matrix : confusion.shape" + str(confusion.shape)
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
    results_df = pd.DataFrame(results, columns=['numTasks', 'utilization', 'TP', 'TN', 'FP', 'FN', 'accuracy'], index=None)
    results_df = results_df.sort_values(['numTasks', 'utilization'])
    # print(results_df)

    num_TP = results_df['TP'].sum()
    # print("num_TP : ", num_TP)
    # results_df.to_csv(output_path, index=False)

    # total accuracy
    total_accuracy = results_df['accuracy'].mean()
    # print("total accuracy : ", total_accuracy)
    return num_TP, total_accuracy, results_df

def getNumTP(input_path):
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
            assert confusion.shape == (1, 1), "need to check the confusion matrix : confusion.shape" + str(confusion.shape)
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
    # print(results_df)

    num_TP = results_df['TP'].sum()
    # print("num_TP : ", num_TP)
    # results_df.to_csv(output_path, index=False)

    # total accuracy
    total_accuracy = results_df['accuracy'].mean()
    # print("total accuracy : ", total_accuracy)
    return num_TP



def check_correntness_real_linux(proposed_result_path, real_linux_result_path):
    proposed_result = pd.read_csv(proposed_result_path)
    real_linux_result = pd.read_csv(real_linux_result_path)
    # merge the two dataframes with [numCores, numTasks, utilization, tasksetIndex]
    merged = pd.merge(proposed_result, real_linux_result, on=['numCores', 'numTasks', 'utilization', 'tasksetIndex'])

    # merged_debug = merged[(merged['realLinux_schedulability'] == False) & (merged['proposed_schedulability'] == True)]
    # print(merged_debug)
    
    results = []
    configurations = merged[['numTasks', 'utilization']].drop_duplicates()
    for i, (num_tasks, utilization) in configurations.iterrows():
            subset = merged[(merged['numTasks'] == num_tasks) & (merged['utilization'] == utilization)]
            
            # Calculate confusion matrix for the subset
            confusion = confusion_matrix(subset['realLinux_schedulability'], subset['proposed_schedulability'])
            
            # Calculate True Positives (TP), True Negatives (TN), False Positives (FP), and False Negatives (FN)
            if (confusion.shape == (2, 2)):
                TP = confusion[1, 1]
                TN = confusion[0, 0]
                FP = confusion[0, 1]
                FN = confusion[1, 0]
            else:
                assert confusion.shape == (1, 1), "need to check the confusion matrix : confusion.shape" + str(confusion.shape)
                if subset['realLinux_schedulability'].iloc[0] == False:
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
    # print(results_df)

    num_TP = results_df['TP'].sum()
    # print("num_TP : ", num_TP)
    # results_df.to_csv("Table5.csv", index=False)

    # total accuracy
    total_accuracy = results_df['accuracy'].mean()
    # print("total accuracy : ", total_accuracy)
    return total_accuracy, results_df


    
def search_FN_info(input_path, num_tasks, utilization):
    df = pd.read_csv(input_path, sep=",")
    subset = df[(df['numTasks'] == num_tasks) & (df['utilization'] == utilization)]
    FN_subset = subset[(subset['simulator_schedulability'] == True) & (subset['proposed_schedulability'] == False)]
    FN_subset_info = FN_subset[['numTasks', 'utilization', 'tasksetIndex', 'simulator_schedulability', 'proposed_schedulability']]
    FN_subset_info.columns = ['nTasks', 'util', 'tasksetIndex', 'sched(Sim)', 'sched(proposed)']
    # merge subsets of simulator_schedulability and proposed_schedulability
    return FN_subset_info


def get_detail_result(detail_result_dir, num_cores, num_tasks, utilization, taskset_index):
    filepath = os.path.join(detail_result_dir, str(num_cores) + 'cores', str(num_tasks) + 'tasks', str(utilization) + 'utilization')
    filename = str(num_cores) + 'cores_' + str(num_tasks) + 'tasks_' + str(utilization) + 'utilization_' + str(taskset_index) + '_result.csv'
    filepath_filename = os.path.join(filepath, filename)
    detail_df = pd.read_csv(filepath_filename, sep=",", header=None, index_col=0)
    return detail_df


def combine_detail_result(summary_path,  detail_result_dir, combine_detail_result_path):
    summary_df = pd.read_csv(summary_path, sep=",")
    
    results = []
    
    for _, result in summary_df.iterrows():
        if result['simulator_schedulability'] == True:
            num_cores = result['numCores']
            num_tasks = result['numTasks']
            utilization = result['utilization']
            taskset_index = result['tasksetIndex']
            
            detail_df = get_detail_result(detail_result_dir, num_cores, num_tasks, utilization, taskset_index)
            detail_df = detail_df.iloc[1:] # remove the first row
            detail_df['numCores'] = num_cores
            detail_df['numTasks'] = num_tasks
            detail_df['utilization'] = utilization
            detail_df['tasksetIndex'] = taskset_index
            results.append(detail_df)
    
    combined_df = pd.concat(results, axis=0, ignore_index=True)
    # add column names
    combined_df.columns = ['name', 'deadline', 'WCRT(sim)', 'simulator_schedulability', 'WCRT(prop)', 'proposed_schedulability', 'numCores', 'numTasks', 'utilization', 'tasksetIndex']
    # reorder columns
    combined_df = combined_df[['numCores', 'numTasks', 'utilization', 'tasksetIndex', 'name', 'deadline', 'WCRT(sim)', 'simulator_schedulability', 'WCRT(prop)', 'proposed_schedulability']]
    # sort the results by numTasks and utilization
    combined_df = combined_df.sort_values(['numTasks', 'utilization'])
    combined_df.to_csv(combine_detail_result_path, index=False)
    return
    