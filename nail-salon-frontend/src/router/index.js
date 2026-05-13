import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import DateTimeSelect from '../views/DateTimeSelect.vue';
import BookingForm from '../views/BookingForm.vue';
import SuccessResult from '../views/SuccessResult.vue';
import GuidePage from '../views/GuidePage.vue';
import PortfolioPage from '../views/PortfolioPage.vue';
import MenuSelection from '../views/MenuSelection.vue';
import MyBooking from '../views/MyBooking.vue';

import AdminDashboard from '../views/AdminDashboard.vue';

const routes = [
  { path: '/', component: Home },
  { path: '/menu', component: MenuSelection },
  { path: '/book', component: DateTimeSelect },
  { path: '/form', component: BookingForm },
  { path: '/success', component: SuccessResult },
  { path: '/guide', component: GuidePage },
  { path: '/portfolio', component: PortfolioPage },
  { path: '/my-bookings', component: MyBooking },
  { path: '/admin', component: AdminDashboard }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
