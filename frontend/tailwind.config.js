/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#0F766E',
          50: '#F0FDFA',
          100: '#CCFBF1',
          200: '#99F6E4',
          300: '#5EEAD4',
          400: '#2DD4BF',
          500: '#14B8A6',
          600: '#0D9488',
          700: '#0F766E',
          800: '#115E59',
          900: '#134E4A',
        },
        secondary: '#14B8A6',
        cta: '#0369A1',
        background: '#F0FDFA',
        text: '#134E4A',
        // Vehicle states
        vehicle: {
          idle: '#10B981',      // green
          delivering: '#3B82F6', // blue
          faulty: '#EF4444',     // red
          offline: '#6B7280',    // gray
        },
        // Order states
        order: {
          pending: '#F59E0B',    // yellow
          assigned: '#8B5CF6',   // purple
          delivering: '#3B82F6', // blue
          completed: '#10B981',  // green
          cancelled: '#6B7280',  // gray
        },
      },
      fontFamily: {
        heading: ['Cinzel', 'serif'],
        body: ['Josefin Sans', 'sans-serif'],
      },
      spacing: {
        'xs': '0.25rem',  // 4px
        'sm': '0.5rem',   // 8px
        'md': '1rem',     // 16px
        'lg': '1.5rem',   // 24px
        'xl': '2rem',     // 32px
        '2xl': '3rem',    // 48px
        '3xl': '4rem',    // 64px
      },
      boxShadow: {
        'sm': '0 1px 2px rgba(0,0,0,0.05)',
        'md': '0 4px 6px rgba(0,0,0,0.1)',
        'lg': '0 10px 15px rgba(0,0,0,0.1)',
        'xl': '0 20px 25px rgba(0,0,0,0.15)',
      },
      transitionDuration: {
        '200': '200ms',
        '300': '300ms',
      },
    },
  },
  plugins: [],
}
