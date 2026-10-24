from typing import Type, Dict

from decision_maker.decision_maker import DecisionMaker


class DecisionMakerFactory:
    def __init__(self) -> None:
        self.decision_makers: Dict[str, DecisionMaker] = {}

    def register_and_create_decision_maker(self, strategy: str, decision_maker_class: Type[DecisionMaker], **kwargs) -> DecisionMaker:
        self.decision_makers[strategy] = decision_maker_class(**kwargs)
        return self.decision_makers[strategy]

    def get_decision_maker(self, strategy: str) -> DecisionMaker:
        if strategy not in self.decision_makers.keys():
            raise ValueError
        return self.decision_makers[strategy]
