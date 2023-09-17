/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  theme: {
    extend: {
      "colors": {
        "gray": {
          50: "#FFFFFF",
          100: "#F0F1F5",
          200: "#CED3DE",
          300: "#AFB7CA",
          400: "#919CB6",
          500: "#7280A1",
          600: "#586584",
          700: "#434D65",
          800: "#2F3646",
          900: "#1A1E27",
          950: "#161A22"
        }
      }
    }
  },
  plugins: [],
}
