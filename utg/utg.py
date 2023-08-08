import os
import json

from llm import openai
from code_parser import java
from util import *

class utg:
    def empty(self):
        return True

if __name__ == '__main__':
    dir_path = os.path.dirname(__file__)
    p = java.parser(f'{dir_path}/code_parser/testdata/ContentWriter.java')
    c = openai.client()

    # get all functions
    print('========= Start parsing functions in Java file =========')
    ans = c.SourceToFunctions(p.get_codes_str())
    ans = json.loads(ans)
    package = ans['Package']
    class_name = ans['Class']
    functions = ans['Functions']
    functions = filter_functions(class_name, functions)
    print(ans)

    # program ut for one function
    print('========= Programming UT for functions =========')
    for f in functions:
        print(f'>>> Writing UT for function: {f}')
        file_content = c.SourceFunctionToUt(p.get_codes_str(), f)
        java.java_file(file_content)
        print(file_content)
