/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  theme: {
    extend: {
      "colors": {
        "gray": {
          50: "#EEF1F6",
          100: "#DEE3ED",
          200: "#BEC7DA",
          300: "#A2AFC8",
          400: "#8494B3",
          500: "#67799D",
          600: "#53617E",
          700: "#414A5E",
          800: "#2E3440",
          900: "#1D222A",
          950: "#161A22"
        }
      }
    }
  },
  plugins: [],
}
