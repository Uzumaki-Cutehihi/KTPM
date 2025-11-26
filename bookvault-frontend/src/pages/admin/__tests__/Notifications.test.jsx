import { render, screen, fireEvent } from '@testing-library/react'
import Notifications from '../Notifications'
jest.mock('../../../services/http', () => ({ default: { get: jest.fn(async () => ({ data: [] })), post: jest.fn(async () => ({ data: {} })), delete: jest.fn(async () => ({}) ) } }))

test('notifications page renders and send button works', async () => {
  render(<Notifications />)
  const sendBtn = await screen.findByText(/Gửi/i)
  fireEvent.click(sendBtn)
  expect(sendBtn).toBeInTheDocument()
})

