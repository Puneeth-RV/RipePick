import tensorflow as tf
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras import layers, Model
import numpy as np

def create_mobilenet_model():
    base_model = MobileNetV2(
        weights='imagenet',
        include_top=False,
        input_shape=(224, 224, 3)
    )
    
    base_model.trainable = False
    
    inputs = tf.keras.Input(shape=(224, 224, 3))
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)
    x = base_model(x, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(128, activation='relu')(x)
    x = layers.Dropout(0.2)(x)
    outputs = layers.Dense(4, activation='softmax')(x)
    
    model = Model(inputs, outputs)
    return model

model = create_mobilenet_model()

model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

print("MobileNetV2 Model Architecture:")
model.summary()

print("\nCreating sample data...")
x_train = np.random.random((32, 224, 224, 3))
y_train = tf.keras.utils.to_categorical(np.random.randint(0, 4, 32), 4)

print("Training demonstration...")
history = model.fit(x_train, y_train, epochs=1, batch_size=8)

print("MobileNetV2 model created and trained successfully!")
print("This model is ready for fruit freshness detection with real data.")