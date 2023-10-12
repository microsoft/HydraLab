import os

import regex


def str_to_lines(code):
    codes = code
    if isinstance(codes, str):
        codes = codes.split("\n")
    return codes


def lines_to_str(codes):
    s = ""
    if isinstance(codes, str):
        s = codes
    else:
        s = " ".join(codes)
    s = s.replace("\n", " ")
    s = regex.sub(r"\s+", " ", s)
    return s.strip()


def filter_strs(strs):
    return [s.strip() for s in strs if s and s.strip() != ""]


def remove_comments(inputs):
    codes = str_to_lines(inputs)
    is_comment_block = False
    ln = 0
    while ln < len(codes):
        line = codes[ln]
        if is_comment_block:
            block_close = line.find("*/")
            if block_close == -1:
                codes[ln] = ""
                ln += 1
                continue
            else:
                codes[ln] = line[block_close + 2 :]
                is_comment_block = False
                continue
        comment_block_start = line.find("/*")
        if comment_block_start >= 0:
            codes[ln] = line[:comment_block_start]
            is_comment_block = True
            ln += 1
            continue
        comment_line_start = line.find("//")
        if comment_line_start >= 0:
            codes[ln] = line[:comment_line_start]
            ln += 1
            continue
        ln += 1
    return codes


java_package_regex = r"package\s+([\w\.\*]+);"
java_import_regex = r"import\s+((static\s+)?([\w\.\*]+));"
java_class_regex = r"((\@[\w\.\(\)]+\s+)*)(public\s+(final\s+)?class)\s+(\w+)\s*(\{((?>[^{}]+|(?6))*)})"
java_function_regex = r"((\@[\w\.\(\)]+\s+)*)(public\s+\w+)\s+(\w+)\s*\(\s*((\w+\s+\w+\s*(\,\s*\w+\s+\w+\s*)*)?)\)\s*(throws\s+(\w+)\s*)?(\{((?:[^{}]*|(?10))*)\})"
java_member_regex = (
    r"((\@[\w\.\(\)]+\s*)*)(\s*(public|private)\s+\w+)\s+(\w+)\s*(=\s*(\w+))?\s*;"
)


class java_file:
    def __init__(self, codes):
        self.package = ""
        self.imports = []
        self.classes = []
        self.__parse(codes)

    def id(self):
        return f"{self.package}"

    def bind(self, rhs):
        imports = []
        [imports.append(i) for i in self.imports + rhs.imports if i not in imports]
        self.imports = imports
        class_dict = {}
        for c in self.classes:
            if class_dict.keys().__contains__(c.id()):
                class_dict[c.id()].bind(c)
            else:
                class_dict[c.id()] = c
        for c in rhs.classes:
            if class_dict.keys().__contains__(c.id()):
                class_dict[c.id()].bind(c)
            else:
                class_dict[c.id()] = c
        self.classes = list(class_dict.values())

    def get_codes_str(self):
        code_str = ""
        if self.package:
            code_str += f"package {self.package};\n\n"
        for i in self.imports:
            code_str += f"import {i};\n"
        if len(self.imports) > 0:
            code_str += "\n"
        for c in self.classes:
            code_str += c.get_codes_str()
        return code_str

    def __parse(self, codes):
        code = lines_to_str(remove_comments(codes))
        # package
        p = regex.match(java_package_regex, code, regex.DOTALL)
        if p:
            self.package = m.group(1).strip()
        # import
        for i in regex.finditer(java_import_regex, code, regex.DOTALL):
            self.imports.append(i.group(1).strip())
        # class
        self.classes = java_class.try_parse_all(code)


class java_class:
    def __init__(self, codes):
        self.tags = []
        self.prefix = ""
        self.class_name = ""
        self.members = []
        self.functions = []
        self.__parse(codes)

    def id(self):
        return f"{self.class_name} {self.class_name}"

    def bind(self, rhs):
        if self.class_name != rhs.class_name:
            return
        member_dict = {}
        for m in self.members + rhs.members:
            member_dict[m.id()] = m
        self.members = list(member_dict.values())
        self.functions = self.functions + rhs.functions

    def get_codes_str(self):
        code_str = ""
        for t in self.tags:
            code_str += f"@{t}\n"
        code_str += f"{self.prefix} {self.class_name} {{\n"
        for m in self.members:
            code_str += f"{m.get_codes_str()}\n"
        for f in self.functions:
            code_str += f"{f.get_codes_str()}\n"
        code_str += "}\n"
        return code_str

    def try_parse(codes):
        try:
            m = java_class(codes)
        except Exception as ex:
            return None
        return m

    def try_parse_all(codes):
        code = lines_to_str(codes)
        classes = []
        for c in regex.finditer(java_class_regex, code, regex.DOTALL):
            class_obj = java_class.try_parse(code[c.start() : c.end()])
            if class_obj:
                classes.append(class_obj)
        return classes

    def __parse(self, codes):
        code = lines_to_str(codes)
        m = regex.match(java_class_regex, code, regex.DOTALL)
        if not m:
            raise ValueError(f"Class parse error:\n{code}")
        self.tags = filter_strs(m.group(1).split("@"))
        self.prefix = m.group(3).strip()
        self.class_name = m.group(5).strip()
        content = m.group(6)
        self.members = java_member.try_parse_all(content)
        self.functions = java_function.try_parse_all(content)


class java_member:
    def __init__(self, codes):
        self.tags = []
        self.prefix = ""
        self.member_name = ""
        self.member_default = ""
        self.__parse(codes)

    def id(self):
        return f"{self.prefix} {self.member_name}"

    def get_codes_str(self):
        code_str = ""
        for t in self.tags:
            code_str += f"@{t}\n"
        code_str += f"{self.prefix} {self.member_name}"
        if self.member_default:
            code_str += f" = {self.member_default}"
        code_str += ";"
        return code_str

    def try_parse(codes):
        try:
            m = java_member(codes)
        except Exception as ex:
            return None
        return m

    def try_parse_all(codes):
        code = lines_to_str(codes)
        members = []
        for m in regex.finditer(java_member_regex, code, regex.DOTALL):
            member = java_member.try_parse(code[m.start() : m.end()])
            if member:
                members.append(member)
        return members

    def __parse(self, codes):
        code = lines_to_str(codes)
        m = regex.match(java_member_regex, code, regex.DOTALL)
        if not m:
            raise ValueError(f"Member parse error:\n{code}")
        self.tags = filter_strs(m.group(1).split("@"))
        self.prefix = m.group(3).strip()
        self.member_name = m.group(5).strip()
        self.member_default = m.group(7)


class java_function:
    def __init__(self, codes):
        self.tags = []
        self.prefix = ""
        self.function_name = ""
        self.parameters = ""
        self.throws = ""
        self.content = ""
        self.__parse(codes)

    def id(self):
        return f"{self.prefix} {self.function_name}"

    def get_codes_str(self):
        code_str = ""
        for t in self.tags:
            code_str += f"@{t}\n"
        code_str += f"{self.prefix} {self.function_name}({self.parameters}) {{\n"
        code_str += f"{self.content}\n"
        code_str += "}\n"
        return code_str

    def try_parse(codes):
        try:
            f = java_function(codes)
        except Exception as ex:
            return None
        return f

    def try_parse_all(codes):
        code = lines_to_str(codes)
        functions = []
        for f in regex.finditer(java_function_regex, code, regex.DOTALL):
            function = java_function.try_parse(code[f.start() : f.end()])
            if function:
                functions.append(function)
        return functions

    def __parse(self, codes):
        code = lines_to_str(codes)
        f = regex.match(java_function_regex, code, regex.DOTALL)
        if not f:
            raise ValueError(f"Funtion parse error:\n{code}")
        self.tags = filter_strs(f.group(1).split("@"))
        self.prefix = f.group(3).strip()
        self.function_name = f.group(4).strip()
        self.parameters = f.group(5)
        self.throws = f.group(9)
        self.content = f.group(11)


class parser:
    def __init__(self, codes, path=""):
        if isinstance(codes, str):
            f = open(codes, "r")
            codes = f.readlines()
            codes = [item.strip() for item in codes]
            f.close()
        self.codes = codes
        self.f_codes = []
        self.is_formatted = False
        self.p_codes = {}
        self.is_parsed = False

    def format_code(self):
        codes = self.codes
        codes = remove_comments(codes)
        codes = [l.strip() for l in codes]
        self.f_codes = [l for l in codes if l]
        self.is_formatted = True

    def parse_code(self):
        if not self.is_formatted:
            self.format_code()
        try:
            codes = self.f_codes
            parser.__parse_code(codes)
            self.is_parsed = True
        except ex:
            pass

    def get_codes(self):
        if not self.is_formatted:
            self.format_code()
        return self.f_codes

    def get_codes_str(self):
        if not self.is_formatted:
            self.format_code()
        code_str = ""
        for c in self.f_codes:
            code_str += c + "\n"
        return code_str


if __name__ == "__main__":
    dir_path = os.path.dirname(__file__)
    p = parser(f"{dir_path}/../testdata/ContentWriter.java")
    codes = p.get_codes()
