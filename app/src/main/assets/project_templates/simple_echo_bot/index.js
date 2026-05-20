module.exports = async function handleMessage(message) {
  if (!message?.content) return;
  if (message.content.startsWith('!echo ')) {
    return message.reply(message.content.slice(6));
  }
};
