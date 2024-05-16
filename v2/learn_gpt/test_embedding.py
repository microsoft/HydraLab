from langchain_openai import AzureOpenAIEmbeddings
import os
from dotenv import load_dotenv, find_dotenv
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.metrics import pairwise_distances

_ = load_dotenv(find_dotenv())


def azure_embedding():
    return AzureOpenAIEmbeddings(
        openai_api_version="2024-02-01",
        azure_deployment="embedding-small",
        api_key=os.getenv('AZURE_GPT_API_KEY'),
        azure_endpoint=os.getenv('AZURE_GPT_ENDPOINT')
    )


def azure_embedding_large():
    return AzureOpenAIEmbeddings(
        openai_api_version="2024-02-01",
        azure_deployment="embedding-large",
        api_key=os.getenv('AZURE_GPT_API_KEY'),
        azure_endpoint=os.getenv('AZURE_GPT_ENDPOINT')
    )


def azure_embedding_ada():
    return AzureOpenAIEmbeddings(
        openai_api_version="2024-02-01",
        azure_deployment="embedding-ada-002",
        api_key=os.getenv('AZURE_GPT_API_KEY'),
        azure_endpoint=os.getenv('AZURE_GPT_ENDPOINT')
    )


def test_embedding(embeddings):
    print("Testing embedding model:", embeddings.model)
    text = "男人"
    nanren = embeddings.embed_query(text)
    print(f"What is nanren? 真男人在此: {nanren}")
    print(f"nanren len: {len(nanren)}")
    text = "高个子"
    gaoge = embeddings.embed_query(text)
    text = "温柔"
    wenrou = embeddings.embed_query(text)
    text = "女人"
    nvren = embeddings.embed_query(text)
    text = "神"
    god = embeddings.embed_query(text)
    text = "猫"
    mao = embeddings.embed_query(text)
    text = "狗"
    gou = embeddings.embed_query(text)
    print("男人女人：相似程度> ", cosine_similarity([nanren], [nvren]), ", l2距离> ",
          pairwise_distances([nanren], [nvren], metric='euclidean'))
    print("猫和狗：相似程度>", cosine_similarity([mao], [gou]), ", l2距离> ", pairwise_distances([mao], [gou], metric='euclidean'))
    print("男人和神：相似程度>", cosine_similarity([nanren], [god]), ", l2距离> ", pairwise_distances([nanren], [god], metric='euclidean'))
    print("女人和神：相似程度>", cosine_similarity([god], [nvren]), ", l2距离> ", pairwise_distances([god], [nvren], metric='euclidean'))
    print("男人和高个子：相似程度>", cosine_similarity([nanren], [gaoge]), ", l2距离> ", pairwise_distances([nanren], [gaoge], metric='euclidean'))
    print("女人和高个子：相似程度>", cosine_similarity([nvren], [gaoge]), ", l2距离> ", pairwise_distances([nvren], [gaoge], metric='euclidean'))
    print("男人和温柔：相似程度>", cosine_similarity([nanren], [wenrou]), ", l2距离> ", pairwise_distances([nanren], [wenrou], metric='euclidean'))
    print("女人和温柔：相似程度>", cosine_similarity([nvren], [wenrou]), ", l2距离> ", pairwise_distances([nvren], [wenrou], metric='euclidean'))
    print("男人和狗：相似程度>", cosine_similarity([nanren], [gou]), ", l2距离> ", pairwise_distances([nanren], [gou], metric='euclidean'))
    print("女人和狗：相似程度>", cosine_similarity([nvren], [gou]), ", l2距离> ", pairwise_distances([nvren], [gou], metric='euclidean'))
    print("男人和猫：相似程度>", cosine_similarity([nanren], [mao]), ", l2距离> ", pairwise_distances([nanren], [mao], metric='euclidean'))
    print("女人和猫：相似程度>", cosine_similarity([nvren], [mao]), ", l2距离> ", pairwise_distances([nvren], [mao], metric='euclidean'))

    text = "朝辞白帝彩云间"
    test_text = embeddings.embed_query(text)
    print(f"test_text len: {len(test_text)}")
    text = """
    Along with twenty-seven members of the European Union including France, Germany, Italy, as well as countries like the United Kingdom, Canada, Japan, Korea, Australia, New Zealand, and many others, even Switzerland.
    We are inflicting pain on Russia and supporting the people of Ukraine. Putin is now isolated from the world more than ever.
    Together with our allies –we are right now enforcing powerful economic sanctions.
    We are cutting off Russia’s largest banks from the international financial system.
    Preventing Russia’s central bank from defending the Russian Ruble making Putin’s $630 Billion “war fund” worthless.
    We are choking off Russia’s access to technology that will sap its economic strength and weaken its military for years to come.
    """
    test_text = embeddings.embed_query(text)
    print(f"long text len: {len(test_text)}")


test_embedding(azure_embedding_ada())
test_embedding(azure_embedding())
test_embedding(azure_embedding_large())
