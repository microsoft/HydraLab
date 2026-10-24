class Device:
    def __init__(self, identifier, platform_name, platform_version):
        self.identifier = identifier
        self.platform = platform_name
        self.platform_version = platform_version

    def __str__(self):
        return f"[{self.platform}]{self.identifier}"

    def __hash__(self):
        return hash(self.__str__())
