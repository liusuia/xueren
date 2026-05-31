<template>
  <div class="ep-root">
    <div class="ep-grid">
      <button v-for="e in emojis" :key="e" class="ep-item" @click="$emit('select', e)" :title="e">{{ e }}</button>
    </div>
  </div>
</template>

<script setup>
defineEmits(['select', 'close'])

const emojis = [
  '😀','😃','😄','😁','😅','😂','🤣','😊',
  '😇','🙂','😉','😌','😍','🥰','😘','😗',
  '😋','😛','😜','🤪','😝','🤑','🤗','🤭',
  '🤔','🤐','😐','😑','😶','😏','😒','🙄',
  '😬','😮','😯','😲','😳','🥺','😢','😭',
  '😤','😡','🤬','😈','👿','💀','☠️','💩',
  '👍','👎','👏','🙌','💪','🤝','👋','✌️',
  '❤️','🧡','💛','💚','💙','💜','🖤','🤍',
  '🔥','⭐','🌟','✨','💯','🎉','🎊','🌸',
  '🐶','🐱','🐼','🐨','🦊','🐰','🐸','🐵',
  '🍎','🍊','🍋','🍉','🍇','🍓','🍒','🍑',
  '☕','🍵','🍺','🍻','🎂','🍰','🍕','🍔',
  '⚽','🏀','🎮','🎵','🎶','📷','💻','📱'
]
</script>

<style scoped>
.ep-root {
  border-top: 1px solid var(--border, #2e3038);
  padding: 8px 12px; max-height: 200px; overflow-y: auto;
}
.ep-grid {
  display: grid; grid-template-columns: repeat(12, 1fr); gap: 4px;
}
.ep-item {
  width: 100%; aspect-ratio: 1; font-size: 18px;
  border: none; background: transparent; cursor: pointer;
  border-radius: 4px; display: flex; align-items: center; justify-content: center;
  transition: background 0.12s;
}
.ep-item:hover { background: var(--bg-hover, rgba(255,255,255,0.08)); }
</style>
