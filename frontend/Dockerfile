# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npm run build

FROM nginx:1.28-alpine AS runtime
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
