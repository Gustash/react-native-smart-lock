# React Native Smart Lock

This package exposes Google Smart Lock for auto sign-in based on saved credentials.

![](https://media.giphy.com/media/gk9yU1AwxQ3HsXSWOY/giphy.gif)

## Getting started

`$ npm install @gustash/react-native-smart-lock --save`

### Mostly automatic installation (RN < 0.60)

`$ react-native link @gustash/react-native-smart-lock`

## Usage

### Request Credentials
```javascript
import SmartLock from '@gustash/react-native-smart-lock';

async function request() {
  // You can also send a client server id as a paramenter as well
  // const {error, success, ...credentials} = await SmartLock.request('some.server.id');
  const {error, success, ...credentials} = await SmartLock.request();

  if (error) {
    console.error(error);
    return;
  }

  if (success) {
    // Account found. Do something with credentials.
    const {
      id,
      password,
      accountType,
      familyName,
      givenName,
      name,
      profilePictureUri,
      idTokens,
    } = credentials;
  }
}
```

### Save Credentials
```javascript
import SmartLock from '@gustash/react-native-smart-lock';

async function save() {
  const credentials = {
    id: 'some@email.com',
    profilePictureUri:
      'https://api.adorable.io/avatars/285/abott@adorable.png',
    name: 'John Doe',
    password: 'password',
  };

  const error = await SmartLock.save(credentials);

  // The save method might be canceled if the system wants to give priority
  // to the password manager UI instead.
  if (error && error !== 'canceled') {
    console.error(error);
  }
}
```

### Delete Credentials
```javascript
import SmartLock from '@gustash/react-native-smart-lock';

async function deleteCredential() {
  const error = await SmartLock.delete('some@email.com');

  if (error) {
    console.error(error);
  }
}
```

### Disable Auto Sign-in
```javascript
import SmartLock from '@gustash/react-native-smart-lock';

// This disables request() until a new save() is called.
// You should call this on logout to allow the user to change accounts, for example.
SmartLock.disableAutoSignIn();
```
