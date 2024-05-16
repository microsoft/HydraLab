from langchain_community.vectorstores import Chroma
from langchain_core.prompts import ChatPromptTemplate
from langchain_community.document_loaders.generic import GenericLoader
from langchain_community.document_loaders.parsers import LanguageParser
from langchain_text_splitters import Language
from langchain_community.document_loaders import DirectoryLoader
from langchain_openai import AzureOpenAIEmbeddings
import os
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.embeddings.sentence_transformer import (
    SentenceTransformerEmbeddings,
)
from langchain.retrievers.multi_query import MultiQueryRetriever
from dotenv import load_dotenv, find_dotenv
from langchain_openai import AzureChatOpenAI
import logging

logging.basicConfig()
logging.getLogger('langchain.retrievers.multi_query').setLevel(logging.INFO)

_ = load_dotenv(find_dotenv())


def print_docs(title, docs):
    print(f"\n======{title}=======")
    for index, doc in enumerate(docs):
        print(f"=========FOUND DOC_{index}==========")
        print(doc.page_content)
        print("Metadata:", doc.metadata)


model = AzureChatOpenAI(
    openai_api_version="2024-02-01",
    azure_deployment=os.getenv('AZURE_OPENAI_DEPLOYMENT'),
    temperature=0,
)
embedding_model = AzureOpenAIEmbeddings(
    openai_api_version="2024-02-01",
    azure_deployment="embedding-small"
)


def create_default_embedding():
    return SentenceTransformerEmbeddings(model_name="all-MiniLM-L6-v2")


def create_source_loader(path: str):
    return GenericLoader.from_filesystem(path, glob="*", suffixes=[".java"], parser=LanguageParser())


def create_source_dir_loader(path: str):
    return DirectoryLoader(path, glob="**/*.java")


def create_base_retriever(code_documents, search_count=3):
    db = Chroma.from_documents(documents=code_documents, embedding=create_default_embedding())
    return db.as_retriever(search_kwargs={"k": search_count})


loader = create_source_dir_loader('/Users/bsp/Desktop/GitHub/HydraLab/v2/center/src/main/java/com/microsoft/hydralab/center/controller')
code_docs = loader.load()
print("Code docs:", len(code_docs))

text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=0)
code_splits = text_splitter.split_documents(code_docs)

retriever = create_base_retriever(code_docs)
# retriever_multi = MultiQueryRetriever.from_llm(retriever, llm=model)

query = "Explain how the authentication works in the code."
find_br = retriever.get_relevant_documents(query)
print_docs("BR", find_br)
# find_mqr = retriever_multi.get_relevant_documents(query=query)
# print_docs("MQR", retriever_multi.get_relevant_documents(query=query))
# find_c = compression_retriever.get_relevant_documents(query=query)
# print_docs("Compress", find_c)

concat_doc = "\n===\n".join([doc.page_content for doc in find_br])

prompt_content = """
please answer the following question based on the following text delimited by triple backticks:
{query}
```
{concat_doc}
```
"""

prompt_template = ChatPromptTemplate.from_messages([
    ('system',
     'You are a helpful pdf content comprehension AI bot. You are good at answering question base on the provided content.'),
    ('user', prompt_content)
])

azure_gpt4_text_model_api_key = os.getenv("AZURE_GPT4_TEXT_MODEL_API_KEY")
azure_gpt4_text_model_endpoint = os.getenv("AZURE_GPT4_TEXT_MODEL_ENDPOINT")
azure_gpt4_text_model_deployment = os.getenv("AZURE_GPT4_TEXT_MODEL_DEPLOYMENT")

model_gpt_4 = AzureChatOpenAI(
    openai_api_version="2024-02-01",
    azure_deployment=azure_gpt4_text_model_deployment,
    azure_endpoint=azure_gpt4_text_model_endpoint,
    api_key=azure_gpt4_text_model_api_key
)

chain = prompt_template | model_gpt_4

print("result: ", chain.invoke({"query": query, "concat_doc": concat_doc}))
