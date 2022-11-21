# Hydra Lab Test Runner Refactor Tips

- Remove DeviceTask interface.
- Decouple service with runner:
  - Intake test task + device, execute task, and get result.
  - A standalone library.