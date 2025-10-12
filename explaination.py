# Import required libraries for deep learning
import tensorflow as tf
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras import layers, Model
import numpy as np

# Define function to create MobileNetV2 model for fruit classification
def create_mobilenet_model():
    # Load pre-trained MobileNetV2 model without the top classification layer
    base_model = MobileNetV2(
        weights='imagenet',        # Use weights trained on ImageNet dataset
        include_top=False,         # Remove the original classification layer
        input_shape=(224, 224, 3)  # Define input image size (224x224 pixels, 3 color channels)
    )
    
    # Freeze the base model layers to prevent them from being trained
    base_model.trainable = False
    
    # Define the input layer for our model
    inputs = tf.keras.Input(shape=(224, 224, 3))
    
    # Preprocess input image to match MobileNetV2 requirements
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)
    
    # Pass preprocessed image through the base MobileNetV2 model
    x = base_model(x, training=False)
    
    # Convert 2D feature maps to 1D vector using global average pooling
    x = layers.GlobalAveragePooling2D()(x)
    
    # Add a fully connected layer with 128 neurons
    x = layers.Dense(128, activation='relu')(x)
    
    # Add dropout layer to prevent overfitting
    x = layers.Dropout(0.2)(x)
    
    # Output layer with 4 neurons for our 4 fruit categories
    outputs = layers.Dense(4, activation='softmax')(x)
    
    # Create the complete model connecting inputs to outputs
    model = Model(inputs, outputs)
    return model

# Create the MobileNetV2 model instance
model = create_mobilenet_model()

# Configure the model for training
model.compile(
    optimizer='adam',                      # Use Adam optimizer
    loss='categorical_crossentropy',       # Use categorical crossentropy loss
    metrics=['accuracy']                   # Track accuracy during training
)

# Display the model architecture summary
print("MobileNetV2 Model Architecture:")
model.summary()

# Create dummy training data for demonstration
print("\nCreating sample data...")
# Generate 32 fake images of size 224x224 with 3 color channels
x_train = np.random.random((32, 224, 224, 3))
# Generate random labels for 4 classes (one-hot encoded)
y_train = tf.keras.utils.to_categorical(np.random.randint(0, 4, 32), 4)

# Demonstrate training with dummy data
print("Training demonstration...")
history = model.fit(x_train, y_train, epochs=1, batch_size=8)

# Print success message
print("MobileNetV2 model created and trained successfully!")
print("This model is ready for fruit freshness detection with real data.")