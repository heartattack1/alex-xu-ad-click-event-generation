FROM python:3.11-slim
WORKDIR /app
COPY apps/event-producer/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY apps/event-producer/main.py ./main.py
COPY contracts/click-event.avsc /contracts/click-event.avsc
ENV PYTHONUNBUFFERED=1
CMD ["python", "main.py"]
