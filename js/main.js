// Veld Framework Documentation JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Initialize all components
    initializeNavigation();
    initializeScrollToTop();
    initializeCodeHighlighting();
    initializeMobileMenu();
    initializeSearch();
    initializeAnchors();
    
    console.log('Veld Framework Documentation initialized');
});

// Navigation functionality
function initializeNavigation() {
    // Highlight current page in navigation
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.sidebar-nav a, .nav-menu a');
    
    navLinks.forEach(link => {
        if (link.getAttribute('href') === currentPath || 
            (currentPath.includes('index.html') && link.getAttribute('href') === './')) {
            link.classList.add('active');
        }
    });
    
    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

// Scroll to top button
function initializeScrollToTop() {
    const scrollButton = document.getElementById('scroll-to-top');
    if (!scrollButton) return;
    
    window.addEventListener('scroll', () => {
        if (window.pageYOffset > 300) {
            scrollButton.classList.add('visible');
        } else {
            scrollButton.classList.remove('visible');
        }
    });
    
    scrollButton.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

// Code highlighting with simple syntax highlighting
function initializeCodeHighlighting() {
    const codeBlocks = document.querySelectorAll('pre code');
    
    codeBlocks.forEach(block => {
        // Simple Java highlighting
        const highlighted = highlightJava(block.textContent);
        block.innerHTML = highlighted;
    });
}

// Simple Java syntax highlighting
function highlightJava(code) {
    // Keywords
    const keywords = /\b(public|private|protected|static|final|abstract|class|interface|enum|extends|implements|import|package|return|if|else|for|while|do|switch|case|break|continue|default|try|catch|finally|throw|throws|new|this|super|void|int|double|float|boolean|char|byte|short|long|String|var|const|let|true|false|null)\b/g;
    
    // Strings
    const strings = /"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/g;
    
    // Comments
    const comments = /\/\/.*$|\/\*[\s\S]*?\*\//gm;
    
    // Annotations
    const annotations = /@[A-Za-z][A-Za-z0-9_]*/g;
    
    // Numbers
    const numbers = /\b\d+\.?\d*[fFdDlL]?\b/g;
    
    // Apply highlighting
    let highlighted = code
        .replace(comments, '<span class="comment">$&</span>')
        .replace(annotations, '<span class="annotation">$&</span>')
        .replace(strings, '<span class="string">$&</span>')
        .replace(numbers, '<span class="number">$&</span>')
        .replace(keywords, '<span class="keyword">$&</span>');
    
    return highlighted;
}

// Mobile menu toggle
function initializeMobileMenu() {
    const mobileToggle = document.querySelector('.mobile-menu-toggle');
    const navMenu = document.querySelector('.nav-menu');
    
    if (!mobileToggle || !navMenu) return;
    
    mobileToggle.addEventListener('click', () => {
        navMenu.classList.toggle('active');
    });
    
    // Close menu when clicking outside
    document.addEventListener('click', (e) => {
        if (!mobileToggle.contains(e.target) && !navMenu.contains(e.target)) {
            navMenu.classList.remove('active');
        }
    });
}

// Search functionality
function initializeSearch() {
    const searchInput = document.getElementById('search-input');
    if (!searchInput) return;
    
    let searchTimeout;
    
    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            performSearch(e.target.value);
        }, 300);
    });
}

function performSearch(query) {
    if (query.length < 3) {
        hideSearchResults();
        return;
    }
    
    // Simple content search (in a real implementation, this would use a search index)
    const searchableContent = document.querySelectorAll('h1, h2, h3, h4, p, code');
    const results = [];
    
    searchableContent.forEach(element => {
        const text = element.textContent.toLowerCase();
        if (text.includes(query.toLowerCase())) {
            results.push({
                element: element,
                text: element.textContent.substring(0, 100),
                level: getHeadingLevel(element)
            });
        }
    });
    
    displaySearchResults(results, query);
}

function displaySearchResults(results, query) {
    let resultsContainer = document.getElementById('search-results');
    
    if (!resultsContainer) {
        resultsContainer = document.createElement('div');
        resultsContainer.id = 'search-results';
        resultsContainer.className = 'search-results';
        document.body.appendChild(resultsContainer);
    }
    
    if (results.length === 0) {
        resultsContainer.innerHTML = '<p>No results found for "' + query + '"</p>';
    } else {
        let html = '<h3>Search Results (' + results.length + ')</h3>';
        results.forEach(result => {
            html += '<div class="search-result">';
            html += '<a href="#" onclick="scrollToElement(this); return false;">';
            html += '<span class="search-result-level">' + result.level + '</span>';
            html += '<span class="search-result-text">' + result.text + '...</span>';
            html += '</a>';
            html += '</div>';
        });
        resultsContainer.innerHTML = html;
    }
    
    resultsContainer.style.display = 'block';
}

function hideSearchResults() {
    const resultsContainer = document.getElementById('search-results');
    if (resultsContainer) {
        resultsContainer.style.display = 'none';
    }
}

function scrollToElement(element) {
    const text = element.querySelector('.search-result-text').textContent;
    const target = Array.from(document.querySelectorAll('h1, h2, h3, h4, p'))
        .find(el => el.textContent.includes(text));
    
    if (target) {
        target.scrollIntoView({ behavior: 'smooth' });
        hideSearchResults();
    }
}

function getHeadingLevel(element) {
    if (element.tagName === 'H1') return 'H1';
    if (element.tagName === 'H2') return 'H2';
    if (element.tagName === 'H3') return 'H3';
    if (element.tagName === 'H4') return 'H4';
    return 'Content';
}

// Auto-generate anchor links for headings
function initializeAnchors() {
    const headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
    
    headings.forEach(heading => {
        if (!heading.id) {
            heading.id = heading.textContent
                .toLowerCase()
                .replace(/[^\w\s-]/g, '')
                .replace(/\s+/g, '-');
        }
        
        // Add anchor link
        const anchor = document.createElement('a');
        anchor.href = '#' + heading.id;
        anchor.innerHTML = '¶';
        anchor.className = 'anchor-link';
        anchor.style.marginLeft = '0.5rem';
        anchor.style.opacity = '0.3';
        anchor.style.textDecoration = 'none';
        
        heading.appendChild(anchor);
        
        // Show anchor on hover
        heading.addEventListener('mouseenter', () => {
            anchor.style.opacity = '1';
        });
        
        heading.addEventListener('mouseleave', () => {
            anchor.style.opacity = '0.3';
        });
    });
}

// Copy code functionality
function copyCode(button) {
    const codeBlock = button.nextElementSibling;
    const text = codeBlock.textContent;
    
    navigator.clipboard.writeText(text).then(() => {
        const originalText = button.textContent;
        button.textContent = 'Copied!';
        
        setTimeout(() => {
            button.textContent = originalText;
        }, 2000);
    });
}

// Add copy buttons to code blocks
function addCopyButtons() {
    const codeBlocks = document.querySelectorAll('pre');
    
    codeBlocks.forEach(block => {
        const copyButton = document.createElement('button');
        copyButton.className = 'copy-code-btn';
        copyButton.innerHTML = 'Copy';
        copyButton.onclick = () => copyCode(copyButton);
        
        block.style.position = 'relative';
        copyButton.style.position = 'absolute';
        copyButton.style.top = '0.5rem';
        copyButton.style.right = '0.5rem';
        copyButton.style.background = 'var(--primary-color)';
        copyButton.style.color = 'white';
        copyButton.style.border = 'none';
        copyButton.style.padding = '0.25rem 0.5rem';
        copyButton.style.borderRadius = '4px';
        copyButton.style.cursor = 'pointer';
        copyButton.style.fontSize = '0.75rem';
        
        block.appendChild(copyButton);
    });
}

// Initialize copy buttons after content loads
setTimeout(addCopyButtons, 1000);

// Theme toggle functionality (if needed)
function toggleTheme() {
    document.body.classList.toggle('dark-theme');
    localStorage.setItem('theme', document.body.classList.contains('dark-theme') ? 'dark' : 'light');
}

// Load saved theme
function loadTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-theme');
    }
}

// Performance monitoring
function logPerformance() {
    if ('performance' in window) {
        const navigation = performance.getEntriesByType('navigation')[0];
        console.log('Page load time:', navigation.loadEventEnd - navigation.loadEventStart, 'ms');
    }
}

// Log performance on page load
window.addEventListener('load', logPerformance);