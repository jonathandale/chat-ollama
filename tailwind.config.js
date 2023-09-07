/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  theme: {
    extend: {
      colors: {
        'gray': {
          50: "#EFF1F5",
          100: "#E0E2EB",
          200: "#C0C6D8",
          300: "#A1A9C4",
          400: "#858FB2",
          500: "#66739F",
          600: "#515C80",
          700: "#3D4561",
          800: "#292F42",
          900: "#161923",
          950: "#0C0D13"
        },
      }
    },
  },
  plugins: [],
}
