from typing import List

from pydantic import BaseModel


class ElementRes(BaseModel):
    id: str


class RankedElement(BaseModel):
    element: ElementRes
    reason: str


class ElementListRes(BaseModel):
    element_list: List[RankedElement]
