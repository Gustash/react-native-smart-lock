import { NativeModules, Platform } from 'react-native';

const { SmartLock } = NativeModules;

export default {
  async request(opts = null) {
    if (Platform.OS === 'ios') {
      return;
    }

    if (opts === null) {
      return SmartLock.request(null);
    } else {
      return SmartLock.request(opts);
    }
  },
  disableAutoSignIn() {
    if (Platform.OS === 'ios') {
      return;
    }

    SmartLock.disableAutoSignIn();
  },
  async save(credentials) {
    if (Platform.OS === 'ios') {
      return;
    }

    return SmartLock.save(credentials);
  },
  async delete(id) {
    if (Platform.OS === 'ios') {
      return;
    }

    return SmartLock.delete(id);
  },
};
