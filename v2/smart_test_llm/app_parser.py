from uiautomator2 import Device
import re


def parse_dumpsys_output(output):
    # Initialize the main dictionary to hold parsed data
    parsed_data = {
        "ActivityResolver": {},
        "Permissions": {}
    }

    # Define patterns for sections and data points
    activity_resolver_pattern = re.compile(r'^\s+(Full MIME Types|Wild MIME Types|Schemes):')
    permissions_pattern = re.compile(r'^\s+Permission \[(.+?)\] \((.+?)\):')

    current_section = None
    current_subsection = None

    for line in output.split('\n'):
        # Check for Activity Resolver Table subsections
        if activity_resolver_match := activity_resolver_pattern.match(line):
            current_section = "Activity Resolver Table"
            current_subsection = activity_resolver_match.group(1)
            parsed_data[current_section][current_subsection] = []

        # Check for Permissions
        elif permissions_match := permissions_pattern.match(line):
            current_section = "Permissions"
            permission_name = permissions_match.group(1)
            permission_details = permissions_match.group(2)
            parsed_data[current_section][permission_name] = permission_details

        # Additional patterns and logic for other sections would go here

    return parsed_data
