<template>
  <div class="service-card">
    <div class="service-card__buttons">
      <a @click.prevent="$emit('remove-service', serv.id)" class="service-card__round-btn"
         href="#"
         v-if="isAdmin">
        <i class="far fa-times-circle"/></a>
    </div>
    <img :src="getImgUrl(serv.image)" alt="" class="service-card__img">
    <div class="service-card__info">
      <h3 class="service-card__title">
        <router-link :to="{name: 'service', params:{id: serv.id} }">{{ serv.title }}</router-link>
      </h3>
      <div class="service-card__description" v-html="serv.description"/>
      <router-link :to="{name: 'service', params:{id: serv.id} }"
                   class="btn service-card_btn">Подробнее
      </router-link>
    </div>
  </div>
</template>

<script>
  import AuthenticationService from '@/service/AuthenticationService';

  export default {
    props: {
      serv: {
        type: Object,
        required: true,
      },
      isAdmin: AuthenticationService.isAdmin(),
    },
    methods: {
      getImgUrl(name) {
        // eslint-disable-next-line global-require,import/no-dynamic-require
        return require(`../../public/static/${name}`);
      },
    },
  };
</script>
