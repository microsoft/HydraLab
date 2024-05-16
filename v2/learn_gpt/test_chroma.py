import chromadb
from chromadb.utils import embedding_functions

chroma_client = chromadb.Client()

collection = chroma_client.create_collection(name="page_collection")
# By default chroma will use default embeddings: all-MiniLM-L6-v2
collection.add(
    documents=["Page 1 is a page for shopping cart", "Page 2 is a page for user logging in"],
    metadatas=[{"source": "p1"}, {"source": "p1"}],
    ids=["id1", "id2"]
)

results = collection.query(
    query_texts=["Login page"],
    n_results=1
)

print(results)

default_ef = embedding_functions.DefaultEmbeddingFunction()
val = default_ef(["stay home", "stay hungry"])
print(len(val[0]), len(val[1]), val)
