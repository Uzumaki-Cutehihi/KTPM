import { render, screen, fireEvent } from '@testing-library/react'
import Search from '../Search'
jest.mock('../../services/http', () => ({ default: { get: jest.fn(async () => ({ data: { content: [{ id:1, title:'T', author:'A' }] } })) } }))

test('search renders results', async () => {
  render(<Search />)
  const input = screen.getByPlaceholderText(/Từ khóa/i)
  fireEvent.change(input, { target: { value: 'abc' } })
  const btn = screen.getByText(/Tìm/i)
  fireEvent.click(btn)
  expect(await screen.findByText(/T/i)).toBeInTheDocument()
})

