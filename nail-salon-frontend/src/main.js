import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import i18n from './i18n';
import Vant from 'vant';
import 'vant/lib/index.css';
import './style.css'; 

const app = createApp(App);
app.use(router);
app.use(i18n);
app.use(Vant);
app.mount('#app');
