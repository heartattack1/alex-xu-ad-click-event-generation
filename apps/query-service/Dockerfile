FROM python:3.11-slim
WORKDIR /app
COPY apps/query-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY apps/query-service/main.py ./main.py
ENV UVICORN_HOST=0.0.0.0
ENV UVICORN_PORT=8080
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
