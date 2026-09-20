FROM python:3.12-slim
WORKDIR /app
COPY tests/fixtures/invalid_subject_oidc/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY tests/fixtures/invalid_subject_oidc/server.py .
USER 65532:65532
EXPOSE 8080
CMD ["python", "server.py"]
