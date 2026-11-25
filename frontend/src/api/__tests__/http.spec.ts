import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest'
import { api, ApiError } from '../http'

let fetchMock: Mock<typeof fetch>

function clearCookies() {
  for (const c of document.cookie.split(';')) {
    const name = c.split('=')[0]?.trim()
    if (name) document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT`
  }
}

describe('api', () => {
  beforeEach(() => {
    fetchMock = vi.fn<typeof fetch>()
    vi.stubGlobal('fetch', fetchMock)
    clearCookies()
  })
  afterEach(() => vi.unstubAllGlobals())

  it('prefixes /api and parses JSON', async () => {
    fetchMock.mockResolvedValue(
      new Response('{"a":1}', { status: 200, headers: { 'Content-Type': 'application/json' } }),
    )
    expect(await api('/x')).toEqual({ a: 1 })
    expect(fetchMock.mock.calls[0]![0]).toBe('/api/x')
    expect(fetchMock.mock.calls[0]![1]!.credentials).toBe('same-origin')
  })

  it('sends XSRF header on POST when cookie present', async () => {
    document.cookie = 'XSRF-TOKEN=abc%3D'
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    await api('/x', { method: 'POST', json: { b: 2 } })
    const init = fetchMock.mock.calls[0]![1]!
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('abc=')
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json')
    expect(init.body).toBe('{"b":2}')
  })

  it('omits XSRF header when cookie missing', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    await api('/x', { method: 'POST', json: {} })
    const init = fetchMock.mock.calls[0]![1]!
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBeNull()
  })

  it('returns undefined on 204', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    expect(await api('/x', { method: 'DELETE' })).toBeUndefined()
  })

  it('maps ProblemDetail to ApiError', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({ status: 400, detail: 'Sai', code: 'VALIDATION_ERROR', errors: { email: 'Bắt buộc' } }),
        { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    )
    await expect(api('/x')).rejects.toMatchObject({
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Sai',
      fieldErrors: { email: 'Bắt buộc' },
    })
  })

  it('maps non-JSON error body', async () => {
    fetchMock.mockResolvedValue(
      new Response('<html>Bad Gateway</html>', { status: 502, headers: { 'Content-Type': 'text/html' } }),
    )
    const err = await api('/x').catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({ status: 502, code: undefined })
  })

  it('maps network failure to status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
    await expect(api('/x')).rejects.toMatchObject({ status: 0, message: 'Không kết nối được máy chủ' })
  })

  it('maps empty 502 from dev proxy (backend down) to status 0', async () => {
    fetchMock.mockResolvedValue(new Response('', { status: 502 }))
    await expect(api('/x')).rejects.toMatchObject({ status: 0, message: 'Không kết nối được máy chủ' })
  })

  it('keeps real 502 with ProblemDetail body', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ detail: 'AI bận', code: 'AI_UNAVAILABLE' }), { status: 502 }),
    )
    await expect(api('/x')).rejects.toMatchObject({ status: 502, code: 'AI_UNAVAILABLE' })
  })

  it('passes FormData through without JSON content-type', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    const fd = new FormData()
    fd.append('file', 'x')
    await api('/x', { method: 'POST', body: fd })
    const init = fetchMock.mock.calls[0]![1]!
    expect(init.body).toBeInstanceOf(FormData)
    expect(new Headers(init.headers).get('Content-Type')).toBeNull()
  })
})
