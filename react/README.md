# DeviceManagementPortal

## Get started for dev

Config webpack.config.js to use the right server:

```javascript
const testHostPath = 'your server config'
```

Run command:
```bash
# npm ci only uses the version in the lockfile and produces an error if the package-lock.json and package.json are out of sync.
# Or you can use npm install if you want to update the lockfile.
npm ci
npm run dev
```

## Dependencies

- lodash: https://lodash.com/, https://www.lodashjs.com/
- Material-UI: https://material-ui.com/
- moment.js: https://momentjs.com/docs/#/displaying/format/
- Bootstrap 4: https://getbootstrap.com/docs/4.0/getting-started/introduction/
- Recharts: http://recharts.org/en-US