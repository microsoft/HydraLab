import json
from collections import defaultdict


class DecisionMetricCounter:
    def __init__(self, max_step):
        self.max_step = max_step
        self.max_depth = 0
        self.min_step_entering_max_depth = 0

    def output(self):
        return {"max_depth": self.max_depth, "min_step_entering_max_depth": self.min_step_entering_max_depth,
                "max_step": self.max_step}

    def update_max_depth(self, current_depth, current_step):
        if self.max_depth >= current_depth:
            return
        else:
            self.max_depth = current_depth
            self.min_step_entering_max_depth = current_step


def extract_result(path):
    txt = ""
    with open(path, "r") as f:
        lines = f.readlines()
        for line in lines:
            if line.startswith("smartTestResult:"):
                txt += line

    txt = "[" + txt.replace("smartTestResult:", ",")[1:] + "]"
    res_metric = [res["metric"] for res in json.loads(txt)]
    return res_metric


def display_summary(metric_data):
    summary_for_each_depth = defaultdict(dict)
    for metric in metric_data:
        summary = summary_for_each_depth[metric.get("max_depth")]
        step_sum = summary.get("step_sum")
        test_count = summary.get("test_count")
        if step_sum:
            summary["step_sum"] = step_sum + metric.get("min_step_entering_max_depth")
        else:
            summary["step_sum"] = metric.get("min_step_entering_max_depth")

        if test_count:
            summary["test_count"] = test_count + 1
        else:
            summary["test_count"] = 1

    sum_test_count = sum([summary.get("test_count") for summary in summary_for_each_depth.values()])
    print("Sum test times: ", sum_test_count)
    for step in sorted(summary_for_each_depth.keys()):
        step_summary = summary_for_each_depth[step]
        print("Depth " + str(step) + ":\n- average step: " + str(
            step_summary.get("step_sum") / step_summary.get("test_count")) + "\n- times: " + str(
            step_summary.get("test_count")) + "\n- percentage: " + str(step_summary.get("test_count") / sum_test_count * 100) + "%")


def output_summary(file_path):
    metric_data = extract_result(file_path)
    display_summary(metric_data)


if __name__ == '__main__':
    file_path = "../dfs_multiple_results.txt"
    output_summary(file_path)

    file_path = "../smart_multiple_results.txt"
    output_summary(file_path)
