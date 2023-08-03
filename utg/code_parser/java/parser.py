
import os


class parser:
    def __init__(self, codes):
        if isinstance(codes, str):
            f = open(codes, "r")
            codes = f.readlines()
            codes = [item.strip() for item in codes]
            f.close()
        self.codes = codes
        self.p_codes = []
        self.is_parsed = False

    def parse_code(self):
        codes = self.codes
        parser.remove_comments(codes)
        self.p_codes = [item.strip() for item in codes]
        print(self.p_codes)
        for line in self.code:
            ns = get_namespace(line)
            if ns != '':
                parse_stacks.push('$' + ns)
                continue
            cs = get_class(line)
            if cs != '':
                parse_stacks.push('#' + cs)
                continue
            fn = get_function(line)
            if fn != '':
                parse_stacks.push('@' + fn)
                continue
        self.is_parsed = True

    def remove_comments(codes):
        is_comment_block = False
        ln = 0
        while ln < len(codes):
            line = codes[ln]
            if is_comment_block:
                block_close = line.find('*/')
                if block_close == -1:
                    codes[ln] = ''
                    ln += 1
                    continue
                else:
                    codes[ln] = line[block_close + 2:]
                    is_comment_block = False
                    continue
            comment_block_start = line.find('/*')
            if comment_block_start >= 0:
                codes[ln] = line[:comment_block_start]
                is_comment_block = True
                ln += 1
                continue
            comment_line_start = line.find('//')
            if comment_line_start >= 0:
                codes[ln] = line[:comment_line_start]
                ln += 1
                continue
            ln += 1

    def get_functions():
        pass

    def consume(code):
        ns = get_namespace(line)
        if ns != '':
            parse_stacks.push('$' + ns)
            #continue
        cs = get_class(line)
        if cs != '':
            parse_stacks.push('#' + cs)
            #continue
        fn = get_function(line)
        if fn != '':
            parse_stacks.push('@' + fn)
            #continue

    def get_namespace(line):
        return False

    def get_class(line):
        return False

    def get_function(line):
        return False

if __name__ == '__main__':
    dir_path = os.path.dirname(__file__)
    p = parser(f'{dir_path}/../testdata/ContentWriter.java')
    p.parse_code()
