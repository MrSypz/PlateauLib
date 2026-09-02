import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'PlateauLib',
  description: 'Documentation for PlateauLib, a Fabric mod library',
  base: '/PlateauLib/',
  cleanUrls: true,

  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/' },
      { text: 'Modules', link: '/modules/' },
    ],

    sidebar: {
      '/guide/': [
        {
          text: 'Guide',
          items: [
            { text: 'Introduction', link: '/guide/' },
          ],
        },
      ],
      '/modules/': [
        {
          text: 'Modules',
          items: [
            { text: 'Overview', link: '/modules/' },
            { text: 'UI', link: '/modules/ui/' },
            { text: 'Post-Process', link: '/modules/postprocess/' },
            { text: 'Particles', link: '/modules/particles/' },
            { text: 'Attributes', link: '/modules/attributes/' },
            { text: 'Sync Config', link: '/modules/sync-config/' },
          ],
        },
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/MrSypz/PlateauLib' },
    ],

    search: {
      provider: 'local',
    },
  },
})
