class TestApplication:
    def __init__(self, path, package_name):
        self.path = path
        self.package_name = package_name

    def __str__(self):
        return f"{self.package_name}"
