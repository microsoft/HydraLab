# Smart Explorer

Key components in the flow:
- SmartExplorer: entry class and organizer.
- UIContextExtractor: get raw UI information
- UIPageDecoder: decode the raw information to PageContext
- Navigator: decide what to do on the page, and produce ActionPlan
- Executor: execute the ActionPlan
- PageMemory: save the step and exploration process, data, provide query API.

What we may need to build:

- InstructionParser: it helps break the instruction into steps each with goal to complete.
- AppInfoParser: Get to know an app, and produce its imformation into AppInfo, which can serve as contextual info for exploration.

## Getting Started

```bash
python -m venv venv
source venv/bin/activate # or .\venv\Scripts\Activate.ps1 on Windows
pip install -r requirements.txt
python main.py
```

If you are using the GPT navigator, you need to provide the .env file with the following content:

```properties
AZURE_OPENAI_API_KEY=your-api-key
AZURE_OPENAI_ENDPOINT=your-endpoint
```
## TODO

- [x] Break the instruction into steps
- [ ] Support more actions
- [ ] Support finetune the model
- [ ] Build/Leverages finetune test dataset.
- [ ] Learn screen bert and leverage it.
- [ ] Choose a navigator among
- [ ] Determine the page features
- Compare UI page to see if stay on the same one.
- GPT4V prompt with vision and action.
- Simplify the action plan.
- Instruction parser.
  - take the app description to gpt about task parsing.
- When the UI and instruction are not matched, we need to ask for more information.
- When the UI and instruction is explored, we need to ask for memory to get cached steps and actions after a success run.


