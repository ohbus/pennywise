FROM node:22.14.0-bookworm-slim
ARG MERMAID_CLI_VERSION
RUN apt-get update \
    && apt-get install --no-install-recommends --yes chromium \
    && rm -rf /var/lib/apt/lists/* \
    && npm install --global "@mermaid-js/mermaid-cli@${MERMAID_CLI_VERSION}" \
    && npm cache clean --force
ENV PUPPETEER_SKIP_DOWNLOAD=true
WORKDIR /workspace
ENTRYPOINT ["mmdc"]
