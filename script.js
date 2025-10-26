// Configuration
const FRUIT_CONFIG = {
    'Fresh Apple': {
        icon: '🍎',
        color: '#48bb78',
        message: 'This apple is fresh and perfect for eating!',
        recommendation: 'Store in a cool, dry place.'
    },
    'Spoiled Apple': {
        icon: '🍎',
        color: '#e53e3e',
        message: 'This apple shows signs of spoilage.',
        recommendation: 'Consider composting or discard if moldy.'
    },
    'Fresh Banana': {
        icon: '🍌',
        color: '#d69e2e',
        message: 'This banana is perfectly ripe!',
        recommendation: 'Great for eating now or freezing for smoothies.'
    },
    'Spoiled Banana': {
        icon: '🍌',
        color: '#744210',
        message: 'This banana is overripe or spoiled.',
        recommendation: 'Perfect for banana bread if not moldy.'
    }
};

// DOM Elements
const uploadArea = document.getElementById('uploadArea');
const previewContainer = document.getElementById('previewContainer');
const preview = document.getElementById('preview');
const analyzeBtn = document.getElementById('analyzeBtn');
const results = document.getElementById('results');
const confidenceBadge = document.getElementById('confidenceBadge');
const statusText = document.getElementById('statusText');
const confidenceFill = document.getElementById('confidenceFill');
const recommendation = document.getElementById('recommendation');

// Event Listeners
document.getElementById('imageUpload').addEventListener('change', handleImageUpload);

function handleImageUpload(e) {
    if (e.target.files[0]) {
        const file = e.target.files[0];
        const reader = new FileReader();
        
        reader.onload = function(event) {
            preview.src = event.target.result;
            uploadArea.style.display = 'none';
            previewContainer.style.display = 'block';
            analyzeBtn.disabled = false;
        };
        
        reader.readAsDataURL(file);
    }
}

function resetUpload() {
    uploadArea.style.display = 'block';
    previewContainer.style.display = 'none';
    results.style.display = 'none';
    analyzeBtn.disabled = true;
    preview.src = '';
}

async function analyzeFruit() {
    // Show loading state
    const btnText = analyzeBtn.querySelector('.btn-text');
    const btnLoading = analyzeBtn.querySelector('.btn-loading');
    
    btnText.style.display = 'none';
    btnLoading.style.display = 'inline';
    analyzeBtn.disabled = true;

    // Simulate AI processing delay
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Generate mock results
    const fruits = Object.keys(FRUIT_CONFIG);
    const randomFruit = fruits[Math.floor(Math.random() * fruits.length)];
    const randomConfidence = Math.floor(Math.random() * 30 + 70);
    
    // Update UI with results
    displayResults(randomFruit, randomConfidence);
    
    // Reset button state
    btnText.style.display = 'inline';
    btnLoading.style.display = 'none';
}

function displayResults(fruitType, confidence) {
    const config = FRUIT_CONFIG[fruitType];
    
    // Update status
    statusText.textContent = fruitType;
    statusText.innerHTML = `${config.icon} ${fruitType}`;
    
    // Update confidence
    confidenceBadge.textContent = `${confidence}%`;
    confidenceFill.style.width = `${confidence}%`;
    confidenceFill.style.background = config.color;
    
    // Update recommendation
    recommendation.textContent = config.message;
    recommendation.style.borderLeftColor = config.color;
    
    // Show results
    results.style.display = 'block';
    
    // Smooth scroll to results
    results.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    console.log('RipePick initialized');
});