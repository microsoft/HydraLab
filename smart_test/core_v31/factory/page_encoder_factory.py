from typing import Type, Dict

from screen_comprehension.screen_comprehension import ScreenComprehension


class PageEncoderFactory:
    def __init__(self) -> None:
        self.page_encoders: Dict[str, ScreenComprehension] = {}

    def register_and_create_page_encoder(self, encoding_algorithm: str, page_encoder_class: Type[ScreenComprehension], **kwargs) -> ScreenComprehension:
        self.page_encoders[encoding_algorithm] = page_encoder_class(**kwargs)
        return self.page_encoders[encoding_algorithm]

    def get_page_encoder(self, encoding_algorithm: str) -> ScreenComprehension:
        if encoding_algorithm not in self.page_encoders.keys():
            raise ValueError
        return self.page_encoders[encoding_algorithm]
