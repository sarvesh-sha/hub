FROM repo.dev.optio3.io:5000/optio3-puppeteer:node15.11.0-buster-slim

COPY --chown=pptruser:pptruser package.json package-lock.json /home/pptruser/app/
RUN npm ci && npm cache clean --force && mkdir -p /home/pptruser/scratch

COPY --chown=pptruser:pptruser . /home/pptruser/app/

RUN npm run build

ENV TZ="America/Los_Angeles"

CMD [ "npm" ]
