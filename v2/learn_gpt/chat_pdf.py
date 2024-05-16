from langchain_community.vectorstores import Chroma
from langchain_core.prompts import ChatPromptTemplate
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import LLMChainExtractor
from langchain_openai import AzureOpenAIEmbeddings
import os
from langchain_community.document_loaders import WebBaseLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.embeddings.sentence_transformer import (
    SentenceTransformerEmbeddings,
)
from langchain.retrievers.multi_query import MultiQueryRetriever
from langchain_community.document_loaders import PyPDFLoader
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


def create_pdf_source_splits():
    loader = PyPDFLoader("NaturalLanguageCommandingviaProgram.pdf")
    return loader.load_and_split()


def create_base_retriever(search_count=3):
    db = Chroma.from_documents(documents=create_pdf_source_splits(), embedding=create_default_embedding())
    return db.as_retriever(search_kwargs={"k": search_count})


def create_compress_retriever(compress_model, base_retriever):
    compressor = LLMChainExtractor.from_llm(compress_model)
    return ContextualCompressionRetriever(base_compressor=compressor, base_retriever=base_retriever)


retriever = create_base_retriever()
compression_retriever = create_compress_retriever(model, retriever)
retriever_multi = MultiQueryRetriever.from_llm(retriever, llm=model)

query = "Explain what is Analysis-Retrieval Method"
find_br = retriever.get_relevant_documents(query)
print_docs("BR", find_br)
find_mqr = retriever_multi.get_relevant_documents(query=query)
print_docs("MQR", retriever_multi.get_relevant_documents(query=query))
find_c = compression_retriever.get_relevant_documents(query=query)
print_docs("Compress", find_c)

concat_doc = "\n=====\n".join([doc.page_content for doc in find_br])


def split_web_page(url:str):
    # loader = WebBaseLoader("https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/chatgpt")
    # https://m.thepaper.cn/baijiahao_14875941
    loader = WebBaseLoader(url)
    web_attack_data = loader.load()

    text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=0)
    return text_splitter.split_documents(web_attack_data)


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
