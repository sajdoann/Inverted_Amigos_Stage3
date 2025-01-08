#!/usr/bin/bash
# run from  the *API* directory ./launch_docker.sh
# Ensure the script is being run from the correct directory
echo "Starting the script..."

cd ..

# Get the current working directory
CURRENT_DIR=$(pwd)
echo "Current Directory: $CURRENT_DIR"

# Run Maven build for /QueryEngine
cd "$CURRENT_DIR/QueryEngine"  # Navigate to the QueryEngine directory
echo "Running mvn clean install in $(pwd) directory..."
mvn clean install

# Run Maven for API
cd ../API
echo "Running mvn clean install in $CURRENT_DIR directory..."
mvn clean install

echo "now in $(pwd)"
# run build for docker
docker build -t api:1.0 .


# Define paths relative to the current directory
INVERTED_INDEX_PATH="$CURRENT_DIR/InvertedIndex/datamart"
BOOKS_PATH="$CURRENT_DIR/gutenberg_books"
METADATA_PATH="$CURRENT_DIR/gutenberg_data.txt"

# Print the paths being used
echo "INVERTED_INDEX_PATH: $INVERTED_INDEX_PATH"
echo "BOOKS_PATH: $BOOKS_PATH"
echo "METADATA_PATH: $METADATA_PATH"

# Stop and remove any existing Docker container
docker stop api-container 2>/dev/null || echo "No container to stop."
docker rm api-container 2>/dev/null || echo "No container to remove."

# Run the Docker container with specified paths
# possibly add -d after run for detached version
docker run  \
  -p 8080:8080 \
  -v "$INVERTED_INDEX_PATH:/app/InvertedIndex/datamart" \
  -v "$BOOKS_PATH:/app/gutenberg_books" \
  -v "$METADATA_PATH:/app/gutenberg_data.txt" \
  --name api-container api:1.0

echo "Docker container started. Check logs with: docker logs api-container"

# you have to wait till all books are indexed to search
# search with:  http://localhost:8080/documents/winter
