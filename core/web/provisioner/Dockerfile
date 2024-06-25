FROM node:18.1.0-bullseye-slim

# Install Git because we are pulling NPM package directly from GitHub.
RUN apt-get update && \
    apt-get install -y git inotify-hookable rsync --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ARG NODE_ENV
ENV NODE_ENV $NODE_ENV
COPY package.json package-lock.json /usr/src/app/
ENV ADBLOCK true
RUN npm clean-install && npm cache clean --force
COPY . /usr/src/app

CMD [ "npm" ]
