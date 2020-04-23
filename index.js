import { NativeModules, Platform } from 'react-native';

const { SmartLock } = NativeModules;

const IOS_ERROR = 'not supported on ios';

export default {
  async request(opts = null) {
    if (Platform.OS === 'ios') {
      return { error: IOS_ERROR };
    }

    if (opts === null) {
      return SmartLock.request(null);
    } else {
      return SmartLock.request(opts);
    }
  },
  disableAutoSignIn() {
    if (Platform.OS === 'ios') {
      return IOS_ERROR;
    }

    SmartLock.disableAutoSignIn();
  },
  async save(credentials) {
    if (Platform.OS === 'ios') {
      return IOS_ERROR;
    }

    return SmartLock.save(credentials);
  },
  async delete(id) {
    if (Platform.OS === 'ios') {
      return IOS_ERROR;
    }

    return SmartLock.delete(id);
  },
};
