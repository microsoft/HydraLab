# Hydra Lab Test Runner Refactor Tips

- Remove DeviceTask interface.
- Decouple service with runner:
  - Intake test task + device, execute task, and get result.
  - A standalone library.


1. Remove DeviceTask interface + improve method access.
2. Callback extraction and refine test runner lifecycle
3. Create a standalone library to place the runner codes and refactor the callback class.
4. Device and runner association to support multiple device.