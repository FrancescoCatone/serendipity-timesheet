FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_API_BASE_URL=/api
ARG VITE_SENTRY_DSN=
ARG VITE_SENTRY_ENVIRONMENT=production
ARG VITE_SENTRY_RELEASE=

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_SENTRY_DSN=$VITE_SENTRY_DSN
ENV VITE_SENTRY_ENVIRONMENT=$VITE_SENTRY_ENVIRONMENT
ENV VITE_SENTRY_RELEASE=$VITE_SENTRY_RELEASE

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
