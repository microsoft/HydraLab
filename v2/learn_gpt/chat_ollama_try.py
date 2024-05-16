from langchain_community.chat_models.ollama import ChatOllama


llm = ChatOllama(model="llama3")

response_message = llm.invoke(
    "In bash, how do I list all the text files in the current directory that have been modified in the last month?"
)

print(response_message)