#!/bin/bash

# run with ./launch_docker.sh in Inverted_Amigos_Stage2/API

#mvn clean install
#docker build -t api:1.0

cd ..

CURRENT_DIR=$(pwd)

cd API

DATAMART_PATH="$CURRENT_DIR/datamart2"
TRIE_INDEX_PATH="$CURRENT_DIR/trie_index_by_prefix"
INVERTED_INDEX_PATH="$CURRENT_DIR/InvertedIndex/datamart"
BOOKS_PATH="$CURRENT_DIR/gutenberg_books"
METADATA_PATH="$CURRENT_DIR/gutenberg_data.txt"

docker stop api-container
docker rm api-container

docker run -d \
  -p 8080:8080 \
  -v "$DATAMART_PATH:/app/datamart2" \
  -v "$TRIE_INDEX_PATH:/app/trie_index_by_prefix" \
  -v "$INVERTED_INDEX_PATH:/app/InvertedIndex/datamart" \
  -v "$BOOKS_PATH:/app/gutenberg_books" \
  -v "$METADATA_PATH:/app/gutenberg_data.txt" \
  --name api-container 11radka11/first-image-api:latest