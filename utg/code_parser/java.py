import os
import re


def str_to_lines(code):
    codes = code
    if isinstance(codes, str):
        codes = codes.split("\n")
    return codes


def lines_to_str(codes):
    if isinstance(codes, str):
        return codes
    return "\n".join(codes)


class java_file:
    def __init__(self, codes):
        self.package = ""
        self.imports = []
        self.classes = []
        self.__parse(codes)

    def __parse(self, codes):
        codes = str_to_lines(codes)
        # package
        for c in codes:
            m = re.match(r"package\s+([\w\.\*]+);\s*", c)
            if m:
                self.package = m.group(1)
        # import
        for c in codes:
            m = re.match(r"import\s+((static\s+)?[\w\.\*]+);\s*", c)
            if m:
                self.imports.append(m.group(1))
        # class
        bracket_depth = -1  # -1: is not in function
        class_begin = -1
        class_end = -1
        for i, c in enumerate(codes):
            if bracket_depth < 0:
                m_open = re.match(r"public\s+class\s+(\w+)\s*{\s*", c)
                if m_open:
                    bracket_depth = 1
                    class_begin = i
                    continue
                m_no_open = re.match(r"public\s+class\s+(\w*)\s*", c)
                if m_no_open:
                    bracket_depth = 0
                    class_begin = i
                    continue
                pass  # not match
            else:
                inc = c.count("{")
                dec = c.count("}")
                bracket_depth += inc - dec
                if dec > 0 and bracket_depth == 0:
                    class_end = i
                    class_codes = codes[class_begin : class_end + 1]
                    class_obj = java_class(class_codes)
                    self.classes.append(class_obj)


class java_class:
    def __init__(self, codes):
        self.prefix = ""
        self.class_name = ""
        self.members = []
        self.functions = []
        self.__parse(codes)

    def __parse(self, codes):
        code = lines_to_str(codes)
        m = re.match(r"public\s+class\s+(\w+)[\s\n]*\{(.*)\}", code, re.DOTALL)
        if not m:
            raise ValueError(f"Class parse error:\n{code}")
        self.class_name = m.group(1)
        content = m.group(2)
        while True:
            member = java_member.try_parse(content)
            if member:
                self.members.append(member)
                continue
            function = java_function.try_parse(content)
            if function:
                self.functions.append(function)
                continue
            break


class java_member:
    def __init__(self, codes):
        self.tags = []
        self.prefix = ""
        self.member_name = ""
        self.member_default = ""
        self.remain = None
        self.__parse(codes)

    def try_parse(codes):
        try:
            m = java_member(codes)
        except Exception as ex:
            return None
        return m

    def get_remain(self):
        ret = self.remain
        self.remain = None
        return ret

    def __parse(self, codes):
        code = lines_to_str(codes)
        m = re.match(
            r"(([\s\n]*\@\w+[\s\n]*)*)([\s\n]*(public|private)[\s\n]+\w+)[\s\n]+(\w+)[\s\n]*[=[\s\n]*(\w+)]?[\s\n]*;[\s\n]*(.*)",
            code,
            re.DOTALL,
        )
        if not m:
            raise ValueError(f"Member parse error:\n{code}")
        self.tags = m.group(1).split("@")
        self.prefix = m.group(2)
        self.member_name = m.group(3)
        self.member_default = m.group(4)
        self.remain = m.group(5)


class java_function:
    def __init__(self, codes):
        self.prefix = ""
        self.function_name = ""
        self.parameters = []
        self.content = ""
        self.__parse(codes)

    def try_parse(codes):
        try:
            f = java_function(codes)
        except Exception as ex:
            return None
        return f

    def __parse(self, codes):
        code = lines_to_str(codes)
        m = re.match(
            r"(([\s\n]*\@\w+[\s\n]*)*)([\s\n]*(public|private)[\s\n]+\w+)[\s\n]+(\w+)[\s\n]*[=[\s\n]*(\w+)]?[\s\n]*;[\s\n]*",
            code,
        )
        if not m:
            raise ValueError(f"Funtion parse error:\n{code}")
        self.prefix = m.group(2)
        self.member_name = m.group(3)
        self.member_default = m.group(4)


class java_function_parameter:
    def __init__(self, code):
        self.type = ""
        self.name = ""
        self.default = ""
        self.__parse(codes)

    def __parse(self, codes):
        pass


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
        parser.__remove_comments(codes)
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

    def __remove_comments(codes):
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

    def __parse_code(codes):
        package = [c for c in codes if re.match(r"package\s+.*;", c)]

    def get_functions():
        pass

    def consume(code):
        ns = get_namespace(line)
        if ns != "":
            parse_stacks.push("$" + ns)
            # continue
        cs = get_class(line)
        if cs != "":
            parse_stacks.push("#" + cs)
            # continue
        fn = get_function(line)
        if fn != "":
            parse_stacks.push("@" + fn)
            # continue

    def get_namespace(line):
        return False

    def get_class(line):
        return False

    def get_function(line):
        return False


if __name__ == "__main__":
    dir_path = os.path.dirname(__file__)
    p = parser(f"{dir_path}/../testdata/ContentWriter.java")
    codes = p.get_codes()
