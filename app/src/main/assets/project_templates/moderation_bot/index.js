module.exports = async function handleMessage(message) {
  if (!message?.content) return;
  const banned = ['spam'];
  if (banned.some((word) => message.content.toLowerCase().includes(word))) {
    return message.delete();
  }
};
