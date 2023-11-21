# WCRT analysis for CFS in Linux systems

## Overview

This Java application is specifically designed for task scheduling simulation and Worst-Case Response Time (WCRT) analysis in the context of Linux's Completely Fair Scheduling (CFS). It is important to note that this application is currently under active development and enhancements. The program serves two primary functions: generating random tasksets for experimental purposes and performing simulations and WCRT analysis on these tasksets.

## Features

* **Task Generation**:  Leveraging the "JsonTaskCreator" class, the application can generate tasks based on user-specified parameters.
* **Scheduling Simulation**: Utilizes the "CFSSimulator" class to simulate task scheduling scenarios.
* **WCRT analysis**: analyzes the WCRT of task using the "CFSAnalyzer" class.
* **Result Saving**: Saves detailed analysis and summary results in a specified directory.

## Prerequisites

* **Java Version**: Requires Java 8 or higher.
* **Build Tool**: Uses Gradle for building and managing dependencies.

### Java Libraries

Refer to "app/build.gradle.kts" for detailed dependency information.

### Python Libraries(only for additional analysis)

    pip install pandas matplotlib scikit-learn

## Basic Usage

### Generate Synthetic Dataset

    bash ./script/generate_taskset.sh

### Run the simulation and the WCRT analysis

    bash ./script/run_experiment.sh

### Additional Analysis Tools

* **Time Consumption Analysis** : ./script/time_complexity.ipynb
* **Check correctness** : ./script/correntness.ipynb

For more detailed information and instructions, please refer to the script directory in the project.